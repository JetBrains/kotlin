# Provisioning

`org.jetbrains.kotlin:build-tools-provisioning` downloads tool distributions, unpacks them and installs them into a directory
shared by all builds on the machine. When several threads, class loaders or processes ask for the same distribution at once,
one of them does the work and the others reuse it. It depends on the Kotlin stdlib and the JDK only.

The design, including the installation protocol and why the lock works the way it does, is in
[docs/build-tools/build-tools-provisioning.md](../../../docs/build-tools/build-tools-provisioning.md).

## Usage

Download, unpack and install:

```kotlin
val store = InstallationStore(userHome.resolve(".kotlin/toolchains/nodejs"))
val provisioner = DistributionProvisioner(store, Downloader(offline = isOffline))

val node: Path = provisioner.provision(
    Distribution(
        id = "node-v24.22.1-linux-x64",
        archive = ArchiveSource.Remote(
            RemoteFile(URI("https://nodejs.org/dist/v24.22.1/node-v24.22.1-linux-x64.tar.gz"), sha256 = expectedSha256)
        ),
        extractor = tarGz,                              // see "Archives" below
        setUp = { installation -> fixNpmSymlinks(installation) },
    )
)
```

An archive the build tool has resolved itself, through Gradle dependency resolution for example:

```kotlin
val konanStore = InstallationStore(konanDir, markerFileName = "provisioned.ok")
val konanHome = DistributionProvisioner(konanStore).provision(Distribution(id, ArchiveSource.Local(resolvedArchive), tarGz))
```

The store alone, for anything that is not "download and unpack":

```kotlin
val installation = store.install("tool-1.0") { staging ->
    // fill staging.directory, then return the directory that becomes the installation
    staging.directory
}
```

## Archives

The library unpacks zip on its own (`Archives.zip()`). The JDK has no tar reader and the library takes no dependencies, so a
caller passes its own `ArchiveExtractor` for tar. In KGP that is `unzipTarGz` from `utils/TarUtil.kt`, which carries the
path checks from KT-86605:

```kotlin
val tarGz = ArchiveExtractor { archive, destination -> archive.unzipTarGz(destination) }
```

## What is in it

| Class                                                                      | What it does                                                                 |
|----------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `InstallationStore`, `StagingArea`, `LeftoverPolicy`                       | A shared directory of installations: complete or invisible, installed once   |
| `DistributionProvisioner`, `Distribution`, `ArchiveSource`                 | Gets the archive, unpacks it, runs the set-up, lets the store publish        |
| `Downloader`, `RemoteFile`, `RetryPolicy`                                  | Downloads with resume, retries, mirrors, SHA-256 and an offline mode         |
| `DownloadTransport`, `HttpTransport`, `DownloadRequest`, `DownloadResponse` | Opens a URL; `HttpURLConnection` and `file:` by default                     |
| `ArchiveExtractor`, `Archives`                                             | Unpacking; zip built in                                                      |
| `ProvisioningListener`                                                     | Events for log lines, progress and statistics; the library does not log      |
| `ProvisioningException` and its subclasses                                 | Offline, download failed, checksum, unsafe archive, leftover, lock timeout   |

## Testing

```bash
./gradlew :build-tools:libraries:provisioning:check -q
```

[AGENTS.md](AGENTS.md) has the rules for changing the library and the patterns its tests use.
