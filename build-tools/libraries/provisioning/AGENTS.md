# Provisioning library

Downloads, unpacks and installs tool distributions into a store shared by every build on the machine. [README.md](README.md)
lists the types; the [design doc](../../../docs/build-tools/build-tools-provisioning.md) explains the decisions. The rules for
every Kotlin Build Tools library are in [../AGENTS.md](../AGENTS.md); this file adds what is specific here.

## Rules the code depends on

- **Never open a lock file a second time in the JVM.** On Linux and macOS, closing any descriptor of a locked file releases
  every lock the process holds on it, whoever took it. Only `InstallationLock.withLock` opens the lock file, once, from the
  thread that holds both the class loader's lock and the JVM-wide monitor, which keeps out copies of the library from other
  class loaders. Anything a waiting caller needs to read goes in `<id>.lock.holder`. Lock files are never deleted.
- **An installation never changes after the rename.** The marker is written into the root before publishing. A feature
  that needs to write into an installation needs a change to the design first.
- **A leftover is deleted only when its marker is confirmed absent** (`Files.notExists`). A marker that cannot be read is an
  I/O error, never a leftover.
- **Only the server or the connection is retried.** Wrap a new network call in `transfer {}` so its failure becomes a
  `TransferException`; leave local file operations outside it, so a full disk fails at once.
- **Outside input is checked before anything is written or run on it**: archive entries, produced roots, the root
  directory before `setUp`, file names taken from URLs, redirect targets. Compare real paths, so a symlink cannot lead out.
- **No logging.** New events are `ProvisioningListener` methods with an empty default.
- **No dependencies beyond the stdlib.** That is why tar is the caller's `ArchiveExtractor`.

## Testing

- **No sleeps.** `InstallationStore`, `InstallationLock` and `Downloader` take `sleep`, `nanoTime` and `move` through their
  internal constructors: record the pauses with a fake `sleep`, advance a fake clock.
- **Waiting:** hold the first caller inside its producer with a latch, wait for the second caller's `waitingForLock` event,
  then release the first.
- **A lock held by another process**, from a single thread: lock the file through a separate `FileChannel`, as
  `InstallationLockTest.withFileLockedElsewhere` does, and release it from the fake `sleep`.
- **Anything about other processes** goes in `InstallationStoreProcessesTest`, which runs `InstallingProcess` in child JVMs.
  Its `probe` mode asks from outside whether a lock is taken. When processes must overlap, hold the first one until the
  others print `waiting`, as `produce-when-told` does; JVMs left alone may start far enough apart to run one after another.
- **A second copy of the library**, as with KGP loaded twice in one daemon: `OtherCopyOfTheLibrary` loads it in a class
  loader of its own and drives it through reflection.
- **HTTP:** `DownloaderTest` scripts responses through a `DownloadTransport`; `HttpTransportTest` runs the JDK HTTP server on
  the loopback interface.
- Symlink and POSIX permission tests are `@DisabledOnOs(OS.WINDOWS)`.
- A fix comes with a test that fails without it. Put the bug back once to see the test fail.

## Design doc

A behaviour change updates [the design doc](../../../docs/build-tools/build-tools-provisioning.md) in the same commit: the
protocol steps, the on-disk layout, the downloader bullets, the exceptions table or the Testing section, whichever it touches.

## Commits

Tag library commits `[Build Tools]`.
