package foo

expect sealed class <!LINE_MARKER("descr='Is subclassed by SealedWithPlatformActuals [common] SealedWithPlatformActuals [main] SimpleShared'"), LINE_MARKER("descr='Has actuals in JVM'")!>SealedWithSharedActual<!>()
expect sealed class <!LINE_MARKER("descr='Is subclassed by SimpleShared'")!>SealedWithPlatformActuals<!> : SealedWithSharedActual
