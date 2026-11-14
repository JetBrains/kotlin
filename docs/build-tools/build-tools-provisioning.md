# Design for `build-tools-provisioning`, a library for installing tool distributions

## Glossary

* **Distribution.** A tool as it is published: one archive for one version and one platform, for example
  `kotlin-native-prebuilt-macos-aarch64-2.5.20.tar.gz` or `node-v24.22.1-linux-x64.tar.gz`.
* **Installation.** An unpacked distribution, ready to use, in a directory on the machine.
* **Store.** The directory that holds installations of one kind, shared by every build on the machine: `~/.konan`,
  `~/.kotlin/toolchains/nodejs`.
* **Id.** The name of an installation inside its store. It is a single directory name and encodes everything that makes two
  distributions different: tool, version, platform.
* **Marker.** A file inside an installation that says the installation is complete, empty unless the archive brings one. It is
  the last thing an installation gets.
* **Staging area.** A scratch directory inside the store where an installation is assembled before anyone can see it.
* **Producer.** The code that fills the staging area: usually "download the archive and unpack it".
* **Provisioning.** Getting from a request ("Node.js 24.22.1 for linux-x64") to an installation, by whatever means.

## References

* [Kotlin Build Tools libraries](../../build-tools/libraries/README.md) and their [rules](../../build-tools/libraries/AGENTS.md)
* [KT-91520](https://youtrack.jetbrains.com/issue/KT-91520), the issue for this library
* [KT-86251](https://youtrack.jetbrains.com/issue/KT-86251) and [KT-91499](https://youtrack.jetbrains.com/issue/KT-91499), two bugs
  in the existing Kotlin/Native installers
* [KT-86605](https://youtrack.jetbrains.com/issue/KT-86605), path traversal and symlink abuse in tar extraction, fixed in
  `utils/TarUtil.kt`, and KT-87190, the regression its first fix caused
* Existing code in the Kotlin Gradle plugin: `NodeJsToolchainService` and `NodeJsDistributionInstaller`, `NativeCompilerDownloader`,
  `NativeVersionValueSource`, `KotlinInterprocessDirectoryLock`, `utils/TarUtil.kt`
* The Gradle wrapper (`org.gradle.wrapper.Install`), which installs Gradle distributions under an exclusive file lock and marks
  finished ones with an `.ok` file
* System package managers and language tool caches, compared in
  [What about the patterns of system package managers?](#what-about-the-patterns-of-system-package-managers): dpkg and apt, rpm and
  dnf, pacman, libzypp, Nix, the Go module cache and Cargo

---

## TL;DR

* `build-tools/libraries/provisioning` (artifact `org.jetbrains.kotlin:build-tools-provisioning`, package
  `org.jetbrains.kotlin.buildtools.provisioning`) downloads tool distributions, unpacks them and installs them into a directory shared
  by all builds on the machine. It depends on the stdlib and the JDK only.
* `InstallationStore` is the core. It guarantees that a directory with an installation's name is always a complete installation, and
  that the work to produce it happens once, however many threads, class loaders, builds and processes ask at the same time.
* `Downloader` fetches a file with resume, retries, a checksum and an offline mode. `ArchiveExtractor` is the interface for
  unpacking. The library ships a zip extractor; tar comes from the caller, and KGP passes the one it has in `TarUtil`.
* `DistributionProvisioner` combines the three for the common case. Each part also works alone: Kotlin/Native keeps resolving its
  archive through Gradle and hands the library a local file.
* The design starts from `NodeJsToolchainService`, the most recent provisioning code in KGP, and keeps its protocol.
* This document covers the library only. Moving Kotlin/Native and Node.js provisioning onto it is separate work.

## Problem

KGP puts several external tools on the user's machine. Each one has its own code for it:

| Code | Gets the archive | Unpacks | Guards the shared directory | "Complete" means |
|---|---|---|---|---|
| `NativeCompilerDownloader` | Gradle dependency resolution | temp directory, then rename | nothing; the KT-91499 fix adds a lock | `provisioned.ok` marker |
| `NativeVersionValueSource` | Gradle dependency resolution | system `tar`, in place | lock file inside the installation | `provisioned.ok`, written last |
| `DependencyProcessor` (LLVM, sysroots) | own HTTP code with resume and retries | in place; system `tar` in the compiler, `unzipTarGz` when KGP runs it | one lock for all dependencies | a line in a shared `.extracted` file |
| Node.js, wasm-tools, WASI adapter installers | own HTTP code, three attempts | Gradle `ArchiveOperations`, temp directory, atomic move | lock next to the installation | the executable exists |
| `AbstractSetupTask` (older Node.js, Yarn, Binaryen setup) | Gradle ivy repository | Gradle `ArchiveOperations` | nothing | hash file |

The three web installers are copies of one another. The two Kotlin/Native installers write to the same `~/.konan` and follow
different rules, which is where the recent bugs come from:

* KT-86251: one installer unpacked on top of a distribution the other had installed, while a build was reading it.
* KT-91499: projects configured in parallel each unpacked the same distribution, about a minute each, and the ones that lost the
  final rename failed the build. On Windows the same race shows up as `AccessDeniedException`.

Every piece of this is easy to get slightly wrong: a lock that lives inside the directory it protects, a completeness check that
passes on a half-unpacked tree, a rename that reports "someone else was faster" in three different ways. None of it depends on
Gradle, and none of it is specific to one tool. The library does it once, with tests that force the bad interleavings.

## Anti-goals

The library does not:

* Know any tool. Distribution names, download URLs, the Node.js symlink fix-up and the choice of version belong to the caller.
* Resolve artifacts from Maven or ivy repositories. A build tool has its own resolution with repositories, credentials, proxies and
  verification; when the archive comes from there, the caller passes the resolved file.
* Find pre-installed tools. Asking `node` on the `PATH` for its version is tool-specific and needs no installation.
* Define DSL, properties or defaults. Where the store is, whether the build is offline and how progress is shown are the caller's
  decisions.
* Remove old installations. Nothing in the repository does today, and a safe policy needs usage tracking that is out of scope here.
* Read tar archives. The JDK cannot, and the code KGP has for it needs Apache Commons Compress. The caller supplies a tar
  extractor (see [Archives](#archives)).
* Replace `DependencyProcessor`. That code also runs inside the Kotlin/Native compiler, which cannot depend on a build tools
  library.

## Overview

```kotlin
val store = InstallationStore(directory = userHome.resolve(".kotlin/toolchains/nodejs"))
val provisioner = DistributionProvisioner(store, Downloader(offline = isOffline))

// The caller's tar extractor. In KGP this is `unzipTarGz` from `utils/TarUtil.kt`.
val tarGz = ArchiveExtractor { archive, destination -> archive.unzipTarGz(destination) }

val node: Path = provisioner.provision(
    Distribution(
        id = "node-v24.22.1-linux-x64",
        archive = ArchiveSource.Remote(
            RemoteFile(
                urls = listOf(URI("https://nodejs.org/dist/v24.22.1/node-v24.22.1-linux-x64.tar.gz")),
                sha256 = "…",
            )
        ),
        extractor = tarGz,           // Archives.zip() for the Windows distribution
        setUp = { installation -> fixNpmSymlinks(installation) },
    )
)

// An archive that the build tool has already resolved: only the store and the extractor are involved.
val konanHome: Path = konanStore.install(id = "kotlin-native-prebuilt-macos-aarch64-2.5.20") { staging ->
    tarGz.extract(resolvedArchive, staging.directory)
    staging.directory.resolve("kotlin-native-prebuilt-macos-aarch64-2.5.20")
}
```

The library has four parts:

* `InstallationStore` owns a store directory and the protocol for adding an installation to it.
* `Downloader` fetches a `RemoteFile` to a local path.
* `ArchiveExtractor` unpacks an archive into a directory.
* `DistributionProvisioner` runs the usual sequence: download, unpack, set up, publish.

## What `NodeJsToolchainService` already does

`NodeJsToolchainService` is the newest provisioning code in KGP and was written with Isolated Projects in mind. The library keeps
its approach:

* A request (version and platform) goes in, a provisioned installation comes out. How the request is satisfied is a strategy:
  download, use a pre-installed tool, or disabled.
* Installations live in one machine-wide directory and are reused by every build.
* `NodeJsDistributionInstaller` returns early when the installation is there, takes a lock next to the installation directory,
  checks again, unpacks into a temporary directory and publishes with an atomic move. An existing installation directory is always
  complete.
* A missing distribution in offline mode fails with a message that says so.

The service, its parameters, the strategies and the DSL are Gradle API and stay in KGP. What moves to the library is the body of
`NodeJsDistributionInstaller.install` and its two copies. A few things change on the way:

* Completeness is a marker written by the library, not "the executable exists". The caller can still add its own check.
* The staging directory belongs to the id and is emptied by the next attempt if a build was killed, instead of leaving
  directories behind.
* The download is verified against a checksum when the caller has one, resumes after a dropped connection, and retries with a delay.
* Unpacking goes through an `ArchiveExtractor` instead of Gradle's `ArchiveOperations`.

## Installation store

### On disk

```
<store>/
├── node-v24.22.1-linux-x64/            an installation; the name exists only when it is complete
│   ├── .provisioned                    the marker
│   └── bin/node, ...
├── .locks/
│   ├── node-v24.22.1-linux-x64.lock    one lock file per id
│   └── node-v24.22.1-linux-x64.lock.holder   who holds it: pid@host
├── .downloads/
│   └── node-v24.22.1-linux-x64/        an archive being downloaded; kept after a failure so the next build continues it
└── .staging/
    └── node-v24.22.1-linux-x64/        being assembled; invisible to readers
```

The staging area is inside the store so that publishing is a rename on one file system. Ids are single directory names that do not
start with a dot, so `.locks` and `.staging` cannot clash with an installation. Each id has its own staging directory, so cleaning up
after one id never touches another whose name starts the same way.

### Protocol

`install(id, produce)`:

1. If `<store>/<id>/<marker>` exists, return `<store>/<id>`. This is one file system call and takes no lock.
2. Take the lock for this id: a lock inside the class loader, a monitor shared by the whole JVM, then an exclusive file lock on
   `.locks/<id>.lock` (see [Threads, class loaders, processes](#threads-class-loaders-processes)).
3. Check the marker again. Whoever held the lock before may have installed it.
4. If `<store>/<id>` exists without a marker, it is a leftover. Handle it according to the store's policy (see
   [Leftovers](#leftovers)).
5. Empty the staging directory of this id. Holding the lock means anything in it is from an attempt that has ended.
6. Run `produce` on the staging directory. It returns the directory that should become the installation. The store resolves
   symlinks in that path: a link to a directory inside staging installs the directory, and a path that leads outside is rejected.
7. Write the marker into that directory.
8. Rename it to `<store>/<id>` with an atomic move, and delete the rest of the staging area. If that last deletion fails, the
   installation is still published and returned.
9. Release the locks and return `<store>/<id>`.

If `produce` throws, the staging area is deleted, the exception reaches the caller, and the store looks as it did before. The next
caller starts from step 1. A failure to clean up is attached to that exception as suppressed and never replaces it.

Deleting, here and for leftovers, never follows a symlink or a Windows junction, and it copes with read-only directories, which
archives may contain.

### What this guarantees

* **Readers never see a partial installation.** The installation name appears in one rename, with the marker already inside. An
  installation is never modified or replaced after that, so a build can read it while other builds install other things.
* **The work happens once.** Callers that arrive during an installation wait in step 2 and leave in step 3.
* **Different installations do not wait for each other.** The lock is per id.
* **A killed process leaves nothing that blocks others.** The operating system releases a file lock when its process dies, and a
  staging area without an owner is removed in step 5.
* **The warm path is free.** A build that finds its tools installed does one `stat` per tool.

### Threads, class loaders, processes

Other processes are kept out by a lock on the file. Such a lock belongs to the process, not to a thread, and on Linux and macOS
it has a trap: closing any descriptor of the file releases every lock the process holds on it, whoever took it. If a second
thread of the JVM opened the lock file only to try the lock or to read it, and then closed it, the holder would go on working
without its lock and another process could start a second installation next to it. So a JVM must never have the lock file open
twice.

Threads therefore queue on a lock inside the JVM first, and only the one that has it opens the file. That lock is in a static
map, so it is per class loader, and KGP can be loaded more than once in one Gradle daemon (included builds, `buildSrc`, differing
plugin classpaths). Copies of the library from different class loaders take turns on the monitor of an interned string, which
is one object for the whole JVM. Waiting for another copy this way has no timeout, cannot be interrupted and sends no waiting
event; that is the price of having no other state that all class loaders share.

Waiting is visible. The holder writes `pid@host` into a file next to the lock file, `<id>.lock.holder`, as libzypp does with its
`zypp.pid`. It is a separate file because of the trap above: a waiting caller reads it without touching the lock file. When the
lock is not free at the first attempt, the store tells its listener which lock it is waiting for and who holds it, so a build
log can say "waiting for process 4242 on agent-7" instead of standing still.

By default a caller waits as long as it takes: an installation needs as long as the download and the unpacking need. A store can
be given a lock timeout, after which the caller fails with an exception that names the lock file and the holder. That turns a
process that hangs while holding the lock into a failure someone can act on, where otherwise every build behind it hangs too.

A thread that is interrupted while waiting stops waiting and throws. Calling `install` for an id from inside its own producer
fails at once instead of waiting forever.

### Windows

A rename fails on Windows while another process has a file open inside the directory being moved, and antivirus scanners and
indexers open freshly written files. The JDK reports this as `AccessDeniedException`, and the store retries the final rename on it
a bounded number of times with a growing delay before giving up. The target cannot be the problem: under the lock it does not exist.

### Leftovers

A directory with the installation's name but no marker means something outside the protocol wrote there: an installer of an older
plugin version that unpacks in place, or a user who copied files by hand. The store takes a policy for this:

* `REPLACE` deletes the leftover and installs. This is the default, and it is safe for a store that only this library writes to.
* `FAIL` throws an exception that names the directory and says to delete it.

Before either, the store makes sure the marker is really absent. A marker that cannot be looked at, because a network share
hiccups or the directory is not accessible, is not a missing marker: the installation fails with an I/O error and the directory
stays, since deleting a good installation that other builds run from would be far worse.

For a store with history the marker name is a parameter, so that installations made by earlier plugin versions count as complete:
`~/.konan` uses `provisioned.ok`. Older installers in `~/.konan` also lock somewhere else; how a store excludes them is part of the
Kotlin/Native migration.

A directory that an installer without markers still writes to must not be taken over with `REPLACE`. The current Node.js
installer, for one, considers an installation complete when the executable exists and writes no marker, so to this library all
its installations are leftovers, and replacing them would pull Node.js away from builds that run an older plugin version. Such a
migration needs a new store directory, or the old installations marked first.

### API

```kotlin
public class InstallationStore(
    directory: Path,
    markerFileName: String = ".provisioned",
    leftoverPolicy: LeftoverPolicy = LeftoverPolicy.REPLACE,
    lockTimeoutMillis: Long? = null,     // null: wait for the other installation as long as it takes
    listener: ProvisioningListener = ProvisioningListener.None,
) {
    /** The store directory, as an absolute path. */
    public val directory: Path

    /** The installation for [id] if it is complete. Never locks, never installs. */
    public fun find(id: String): Path?

    /**
     * Returns the complete installation for [id]. If there is none, runs [produce] to make one; concurrent callers
     * for the same [id] wait and get the same installation.
     *
     * [produce] returns the directory that becomes the installation: the staging directory itself or a directory
     * below it. Everything else in the staging directory is deleted.
     */
    public fun install(id: String, produce: (staging: StagingArea) -> Path): Path
}

public class StagingArea internal constructor(public val directory: Path)
```

Letting `produce` return a directory below the staging area covers the usual archive layout, where everything sits in one top-level
directory. An archive a producer downloads into the staging area disappears with it; `DistributionProvisioner` keeps its downloads
in `.downloads` instead, so that they can be continued.

## Downloader

```kotlin
public class RemoteFile(
    public val urls: List<URI>,      // tried in order; the first one that answers wins
    public val sha256: String? = null,
) {
    public constructor(url: URI, sha256: String? = null)
}

public class RetryPolicy(
    public val maxAttempts: Int = 3,           // per URL
    public val initialDelayMillis: Long = 1_000,
    public val maxDelayMillis: Long = 10_000,  // the delay doubles after each failed attempt, up to this
)

public class Downloader(
    offline: Boolean = false,
    retry: RetryPolicy = RetryPolicy(),
    transport: DownloadTransport = HttpTransport(),
    listener: ProvisioningListener = ProvisioningListener.None,
) {
    /** Downloads [file] to [target] and returns [target]. */
    public fun download(file: RemoteFile, target: Path): Path
}
```

* **Partial files.** Bytes go to `<target>.part`. The target name appears by a rename after the checksum has passed, so a file with
  the target name is always whole. A file already at the target is replaced.
* **Resume.** When a `.part` file exists and the server supports ranges, the download continues from its end. The validator of the
  first response (`ETag`, or `Last-Modified` without one) is kept in `<target>.part.validator`, together with the URL, and sent as
  `If-Range`, so a file that changed on the server comes back whole and the download restarts from zero. A server may ignore
  `If-Range`; then the validator in its range response gives the change away. Without a validator nothing is resumed, and bytes
  from one URL are never continued from another. A part file outlives a failed download, so the next build continues it.
* **Retries.** Connection failures, timeouts, truncated bodies and 5xx or 429 answers are retried with a growing delay, up to the
  limit in `RetryPolicy`. Other 4xx answers are not: a 404 will be a 404 next time. A range that does not start where it was asked
  to, or a 416 answer, makes the next attempt start from zero. After the last attempt on one URL the next URL is tried. Only
  failures of the server or the connection are retried. A failure of the local file system, a full disk for example, reaches the
  caller at once and as it is.
* **Checksum.** With `sha256` set, the digest is computed while the bytes are written; a resumed download reads the bytes it already
  has first. A mismatch deletes the partial file. The same URL is not asked again, since it would send the same bytes, but the
  next URL is: a captive portal or a broken mirror should not stop a download that another mirror can serve. When no URL is
  left, `ChecksumMismatchException` carries both values. The exception is a mismatch after a resume: the bytes left on disk by
  an earlier attempt may be the bad ones, so the same URL is downloaded once more from the start.
  Without a checksum, the downloader still checks the length the server announced, and a shorter body counts as truncated.
* **Offline.** With `offline = true` nothing is requested and `OfflineException` names the file that is missing.
* **Transport.** `DownloadTransport` takes a `DownloadRequest` (URL, offset, `If-Range` validator) and returns a `DownloadResponse`
  (status, headers, body). HTTP error statuses are responses, not exceptions. The default, `HttpTransport`, uses
  `HttpURLConnection`, like every downloader in the repository today, with connect and read timeouts, the JVM's proxy settings and
  `Accept-Encoding: identity`, so that lengths and offsets refer to the file itself. It follows up to five redirects itself, only
  to `http` and `https` URLs, and never from `https` to `http`. `file:` URLs work too, with the modification time as the
  validator, which is enough for a local mirror; a server cannot redirect to one.

The downloader has no locking of its own. It is called from a producer, where the store's lock already makes sure one caller
downloads a given distribution.

## Archives

```kotlin
public fun interface ArchiveExtractor {
    /** Unpacks [archive] into [destination], which exists and is a directory. */
    public fun extract(archive: Path, destination: Path)
}

public object Archives {
    /** Unpacks zip archives with the JDK. */
    public fun zip(): ArchiveExtractor
}
```

Unpacking is an interface, and the library implements it for zip only.

The distributions for macOS and Linux are tar archives, and the JDK has no tar reader. The repository already has code for it:

| Code | Does | For the library |
|---|---|---|
| `Path.unzipTarGz` in KGP `utils/TarUtil.kt` | tar.gz with symlinks, hard links and permissions; rejects entries that escape the target (CWE-22) or write through a symlink (CWE-59) | KGP passes it as an `ArchiveExtractor`. It reads tar with Apache Commons Compress, so it cannot move here |
| `Path.unzipTo` in `compiler/util-io` | zip through the JDK zip file system, with a zip-slip check | cannot be used: compiler internals |
| `DependencyExtractor` in `kotlin-native-utils` | tar.gz through the system `tar` | cannot be used: compiler-side module |
| Gradle `ArchiveOperations` | zip and tar trees | cannot be used: Gradle API |
| `copyZipFilePartially` in KGP `utils/zipUtils.kt` | copies a subtree of one zip into another zip | not relevant: it rewrites archives for multiplatform publications and unpacks nothing |

`unzipTarGz` was hardened for KT-86605 and adjusted for KT-87190, where rejecting every outward symlink broke bundles that rely on
them. It has tests for both (`KotlinNativeBundleTarExtractionSecurityTest`). Leaving it where it is keeps that work intact and
keeps the library on the stdlib: KGP embeds Commons Compress in its own jar under a relocated package, and a separately published
library can neither see that copy nor declare the dependency without putting it on every user's buildscript classpath.

With the extractor as a parameter, KGP wraps `unzipTarGz` in one line, as in the [overview](#overview). Another integration passes
whatever tar code it has. If a second integration needs tar and has none, a reader can be added to `Archives` later without
changing the interface.

`Archives.zip()` reads with the JDK's `ZipFile` and rejects any entry whose path, after normalisation, leaves the destination,
or that would reach outside through a symlink already in the destination. The check runs before anything is created, so a
rejected entry leaves not even an empty directory outside. The JDK does not expose the Unix mode of a zip entry, so permissions
and symlinks are not restored. The distributions that ship as zip are the Windows ones, where neither matters.

## `DistributionProvisioner`

```kotlin
public class Distribution(
    public val id: String,
    public val archive: ArchiveSource,
    public val extractor: ArchiveExtractor,
    /** The top-level directory of the archive that becomes the installation; null when the files are at the root. */
    public val rootDirectory: String? = id,
    /** Runs on the unpacked distribution before it is published. */
    public val setUp: (installation: Path) -> Unit = {},
)

public sealed interface ArchiveSource {
    public class Remote(public val file: RemoteFile) : ArchiveSource
    public class Local(public val archive: Path) : ArchiveSource
}

public class DistributionProvisioner(
    store: InstallationStore,
    downloader: Downloader = Downloader(),
    listener: ProvisioningListener = ProvisioningListener.None,  // unpacking events
) {
    public fun provision(distribution: Distribution): Path
}
```

`provision` is `store.install` with a standard producer: get the archive (download it, or use the local file), unpack it, check
that `rootDirectory` is there, run `setUp`, return that directory. `rootDirectory` is a relative path that stays inside the
archive; a missing one fails with a message that lists what the archive has at its top level.

A remote archive is downloaded into `.downloads/<id>` in the store, not into the staging area. The store deletes staging
directories at the start of every attempt, and the downloader's part file has to survive a failure for the next build to continue
it. The download is deleted after the installation is published, and a failure to delete it does not fail the installation. The
file keeps its name from the URL when that is a plain file name, because extractors may go by the extension; otherwise it is
called `archive`. A local archive is only read.

An archive with a checksum that is already in `.downloads/<id>` under its final name and matches is not downloaded again. That
is the case when an earlier attempt got the whole archive and then failed to unpack it.

The root directory is resolved before `setUp` runs: with an extractor that restores symlinks, an archive could make its root a
link to somewhere else, and the set-up must not get to work there.

`setUp` runs before publication, so fix-ups such as the Node.js `npm` symlinks or executable bits are part of the installation by the
time anyone sees it. If `setUp` throws, nothing is published.

The provisioner keeps no memory between calls. The store's first step is cheap enough, and a build tool that wants a per-build cache
(as `DefaultNodeJsToolchainServiceImpl` has) knows its own lifecycle better.

## Listener and errors

```kotlin
public interface ProvisioningListener {
    public fun waitingForLock(id: String, lockFile: Path, holder: String?) {}   // holder: pid@host, if known
    public fun downloadStarted(url: URI, totalBytes: Long?) {}
    public fun downloadProgress(url: URI, bytes: Long, totalBytes: Long?) {}
    public fun downloadFinished(url: URI, bytes: Long, durationMillis: Long) {}
    public fun downloadRetried(url: URI, attempt: Int, cause: IOException) {}
    public fun extractionStarted(archive: Path, destination: Path) {}
    public fun installed(id: String, installation: Path) {}
    public fun leftoverRemoved(installation: Path) {}
}
```

The library does not log. A listener gets events, and the integration turns them into log lines, a progress bar or statistics (KGP
reports download size and duration today). This keeps the library free of a logging dependency and lets each integration choose its
own wording and levels.

Failures the library detects itself are subclasses of `ProvisioningException`, which is an `IOException`:

| Exception | When |
|---|---|
| `OfflineException` | a download is needed and the downloader is offline |
| `DownloadFailedException` | every URL and every attempt failed; carries the URLs tried and the last status or cause |
| `ChecksumMismatchException` | the downloaded bytes do not match `sha256` |
| `UnsafeArchiveException` | an archive entry tries to leave the destination |
| `InvalidDistributionException` | the archive has no `rootDirectory`, the root directory is a link out of the archive, or `produce` returned a path outside the staging area |
| `LeftoverInstallationException` | policy `FAIL` and a directory without a marker is in the way |
| `InstallationLockTimeoutException` | another caller is still installing the same id when the store's lock timeout runs out; carries the lock file and the holder |

Messages name the files and URLs involved and say what the user can do. Exceptions thrown by `produce` or `setUp` pass through
unchanged.

Some failures are not `ProvisioningException`s:

* errors of the local file system, a full disk for example, come as the JDK reports them;
* a marker that cannot be read is an `IOException` that names the installation;
* a missing local archive is a `NoSuchFileException`;
* mistakes of the caller are `IllegalArgumentException`s: an invalid id or marker name, a root directory outside the archive,
  a URL scheme `HttpTransport` does not support.

## What stays in the integration

For orientation only; the migration is not part of this design.

* Building ids and URLs from a version and a platform.
* Choosing the store directory, the marker name and the leftover policy.
* Resolving an archive through the build tool when that is where it comes from.
* The tar extractor, and any other archive format the library does not read.
* Lifecycle: in Gradle, a build service or value source that calls the library. Checking the marker at configuration time through
  plain file access would make it a configuration cache input, so KGP keeps doing it inside a value source.
* Strategies such as "use the pre-installed tool" and the diagnostics around them.

## Module layout

```
build-tools/libraries/provisioning/
├── build.gradle.kts
├── code-rules.md                      @../library-rules.md
├── api/provisioning.api
└── src/
    ├── main/kotlin/org/jetbrains/kotlin/buildtools/provisioning/
    │   ├── InstallationStore.kt           with StagingArea, LeftoverPolicy
    │   ├── InstallationLock.kt            internal: class-loader lock, JVM-wide monitor, file lock, holder file
    │   ├── Downloader.kt
    │   ├── RemoteFile.kt                  with RetryPolicy
    │   ├── DownloadTransport.kt           with HttpTransport
    │   ├── ArchiveExtractor.kt            with Archives and the zip extractor
    │   ├── DistributionProvisioner.kt     with Distribution, ArchiveSource
    │   ├── ProvisioningListener.kt
    │   ├── ProvisioningException.kt       with its subclasses
    │   └── fileUtils.kt                   internal: deleting without following symlinks or junctions
    └── test/kotlin/org/jetbrains/kotlin/buildtools/provisioning/
        ├── <Subject>Test.kt
        ├── OtherCopyOfTheLibrary.kt       the library loaded a second time, in its own class loader
        └── zipTestUtils.kt
```

One package, as the library rules require. The public API is recorded in `api/provisioning.api`. Next to `build.gradle.kts` are
`README.md` for users of the library and `AGENTS.md` (with `CLAUDE.md` pointing to it) for whoever changes it.

## Testing

All tests live in the library and use no Gradle. Files go to `@TempDir`.

The rules forbid sleeps, and races cannot be tested by hoping for the right timing. Tests hold one caller inside its producer with
a latch and learn that another caller is waiting from the listener's `waitingForLock` event, then release the first. What a test
cannot arrange through files, a denied rename for example, comes through internal constructor parameters: the store, the lock and
the downloader take their move, delay and clock functions from outside. The one wait without an event, a copy of the library
from another class loader blocked on the JVM-wide monitor, is detected from the thread's state, in a loop that yields instead
of sleeping.

* `InstallationStoreTest`
  * many threads install one id: `produce` runs once and all get the same path;
  * a caller that arrives while another is producing waits and returns the same installation without producing;
  * two ids install at the same time without waiting for each other;
  * `produce` throws: nothing is published, the staging area is gone, the next call produces again;
  * a staging area left by a "killed" attempt is removed by the next one;
  * leftovers under both policies, a custom marker name, an installation made with the marker already present;
  * `produce` returns the staging directory, a subdirectory, and a path outside (rejected);
  * `produce` returns a symlink to a directory in staging (the directory is installed) or one that leads outside (rejected);
  * a read-only directory in staging does not hide the producer's failure;
  * a marker that cannot be read: the installation is kept, not replaced as a leftover;
  * the lock timeout, with the holder in the exception, and installing an id from inside its own producer;
  * ids with spaces and non-ASCII characters; ids with separators or a leading dot (rejected);
  * `find` on missing, partial and complete installations.
  * a denied rename that succeeds on retry, and one that never does.
* `InstallationStoreProcessesTest` starts four JVMs that install the same id in one store. The one that gets the lock holds it
  until the other three have reported that they wait, so the processes really overlap, and exactly one of them produces. A
  second case kills the JVM that holds the lock in the middle of `produce` and checks that the next one recovers. A third has
  a caller in the holder's own JVM wait and give up, and then asks a separate JVM whether the lock is still taken. A fourth
  loads a second copy of the library in a class loader of its own, as a Gradle daemon with KGP loaded twice would, and checks
  that it blocks on the JVM-wide monitor while the first copy installs, that the holder keeps its file lock meanwhile, and that
  the second copy then returns the installation without producing it.
* `InstallationLockTest` covers a file lock already held elsewhere, by another process or channel, interruption while
  waiting, release after a failing action, the holder's name in the holder file and in the waiting event, and the timeout
  with a fake clock: the pauses between attempts and the exception.
* `FileUtilsTest` covers deleting trees with symlinks, without following them, and with read-only and unreadable directories.
* `DownloaderTest` runs against a scripted `DownloadTransport`: a connection that drops and resumes, a part file left by an earlier
  build, a server without range support or without validators, a file that changes between attempts, wrong ranges and 416, retry
  limits and which statuses are retried, fallback to the second URL, bytes from one URL never continued from another, checksum
  and length mismatch, including a mismatch after a resume, local failures that are not retried, offline mode, and the listener
  events.
* `HttpTransportTest` runs the real transport against the JDK's built-in HTTP server on the loopback interface, which
  `test-inputs-check` allows: ranges and the headers sent, relative redirects and a redirect loop, error bodies, a read timeout, a
  connection closed in the middle of the body, and `file:` URLs. Refusing redirects away from `https` or to `file:`, `ftp:` and
  `jar:` is tested on the redirect logic alone, since the server has no TLS.
* `ArchivesTest` builds zip archives in the test: nested directories with and without their own entries, long and non-ASCII
  names, an empty archive, entries that try to leave the destination by `..` or an absolute path or through a symlink, an entry
  that collides with an existing directory, and a file that is not a zip.
* `DistributionProvisionerTest` covers both archive sources, a root directory that is missing, nested, absent or outside the
  archive or a link out of it, an extractor or `setUp` that fails, `setUp` running before publication, no second unpacking, a
  failed download that the next call continues, a complete download reused when unpacking failed, and file names taken from
  URLs.

The tar extractor is KGP's and keeps its tests there.

## Open questions

1. **Durability.** The store does not `fsync` before the rename, so after a power loss an installation could have its marker and
   truncated files. The installers listed above do not sync either. A caller can pass a check through `setUp`; whether the store
   should offer a `verify` step on the warm path is open.

## FAQ

### Why a marker file instead of checking that the executable exists?

"The executable exists" is true for a half-unpacked tree as soon as the unpacker reaches that file, and it needs tool knowledge the
library does not have. A marker written last means the same thing for every tool. It also lets `~/.konan` keep `provisioned.ok`, so
installations from released plugin versions stay valid.

### Why is the lock not inside the installation directory?

A lock file inside the directory means the directory exists before the installation does. Then the installation name can no longer
appear by one rename, a reader cannot tell "being installed" from "installed", and unpacking has to happen in place. This is how the
Kotlin/Native toolchain installer works, and KT-86251 came out of it.

### Why file locks and not a lock file that exists while the work runs?

A process that is killed cannot delete its lock file, and everyone after it waits for a timeout or forever. An operating system file
lock disappears with its process.

### Why not `REPLACE_EXISTING` in the final rename?

Under the lock the target does not exist, so there is nothing to replace. With the flag, `Files.move` deletes an empty target and then
renames, in two steps, and that gap is where the KT-91499 failure came from.

### Why is there one lock per installation and not one per store?

Unpacking a Kotlin/Native distribution takes about a minute. A build that needs a different version, or another tool from the same
store, should not wait for it.

### Why not reuse `DependencyDownloader` and `DependencyProcessor`?

They belong to `kotlin-native-utils` and are tied to `konan.properties`. They print to standard output, take one lock for all
dependencies and keep the JVM-wide part of it in system properties, and unpack in place. The resume and retry logic is the useful
part, and the downloader here does the same.

### Why doesn't the library download Kotlin/Native itself?

It could: the distributions are on a plain HTTP server. But builds get them through Gradle's dependency resolution today, which gives
users repository mirrors, credentials, proxies, `--offline` and dependency verification for free. `ArchiveSource.Local` keeps all of
that and still gives Kotlin/Native the store.

### Why doesn't the library unpack tar itself?

There were three ways to do it, and each costs more than it gives today:

* Depend on Commons Compress, as KGP's `TarUtil` does. KGP embeds it under a relocated package, so the library would have to be
  embedded into KGP too, or carry a second shaded copy, or put four Apache libraries on the buildscript classpath.
* Write a tar reader on the JDK. That is a parser for ustar, pax and GNU headers to own, and the fixes for KT-86605 and KT-87190
  would have to be proven again on it.
* Start the system `tar`. Then the one security-sensitive step depends on the host and is outside our tests.

KGP is the only consumer for now and already has tested tar code, so the library takes an extractor and stays on the stdlib.

### Why `HttpURLConnection` and not Ktor?

A Ktor client brings an engine, kotlinx-coroutines and kotlinx-io. Under the library rules that needs an API and implementation
split, with KGP loading the implementation in an isolated class loader from artifacts it resolves at build time, so a build could
not provision anything before it has downloaded an HTTP client. What it would add is of little use here: the work is one large
file per distribution, and the network is the bottleneck. `DownloadTransport` is an interface, so another engine can be added
later as a separate module.

### What about the patterns of system package managers?

dpkg, rpm, pacman, libzypp and Nix have installed software onto shared machines for decades, and the Go module cache and Cargo
solve the same problem this library does. Most of what they converged on is in the design already:

| Pattern | Seen in | Here |
|---|---|---|
| write under a temporary name, then rename | dpkg (`.dpkg-new`), rpm, Go, Nix | the whole installation in one rename |
| a marker written last means "complete" | Cargo (`.cargo-ok`), Go (`.partial`), the Gradle wrapper (`.ok`) | the marker file |
| locks that die with their process | apt, libzypp, Nix | file locks; pacman's "the lock file exists" is the counterexample, with its stale `db.lck` |
| clean up temporary files only when a lock proves their writer is gone | Go | the staging directory |
| partial files, resume, start over on 416, retries with backoff | apt | the downloader |
| fall back to the next mirror, also on a wrong checksum | librepo (dnf) | the downloader |
| downloads in a stable place, so they resume | pacman 7 lost this with its random download directories | `.downloads/<id>` |

One pattern was adopted after the comparison: the lock names its holder and can time out, as in libzypp.

The others were weighed against what KGP needs and left out:

* **Checksums required for every download** (apt, Go, Homebrew). The hash has to come from somewhere more trusted than the
  archive. Kotlin/Native archives come through Gradle, which has its own dependency verification. For Node.js the hash list is
  on the same server as the archive, so over HTTPS it adds little. `sha256` stays optional, and an integration that has a
  trusted hash passes it.
* **A record of what was installed, and a verify command** (Go's `.ziphash`, Nix's path hashes). Nothing would call it. The
  failures seen in `~/.konan` were in caches the compiler writes into the distribution after installation, which a hash of the
  archive cannot check.
* **A free-space check before starting** (rpm). The unpacked size of a tar archive is a guess, and rpm's own estimates are known
  to be off. A full disk already fails with the operating system's message, and at once, since local failures are not retried.
* **Removing installations nobody uses** (pacman's `paccache`, Gradle's cache cleanup, Nix's garbage collector). Readers take no
  lock here, which is what makes the warm path free, so a collector cannot know that an installation is not in use. It stays an
  anti-goal until a policy for that exists.
* **Read-only installations** (Nix, Go). The Kotlin/Native compiler writes caches into its distribution.
* **Syncing to disk before the rename** (dpkg, after it lost files in crashes). See the open question on durability.
* **Downloading as an unprivileged user or in a sandbox** (apt, pacman). Not possible from a library inside a build tool's JVM.

### Why a listener and not a BTA `KotlinLogger`?

Progress and timings are data, not text: KGP sends download size and duration to statistics, and a progress bar needs byte counts.
With a logger the integration would have to parse messages. A listener also means the library needs no dependency on BTA.
