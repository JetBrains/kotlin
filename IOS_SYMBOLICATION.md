# Symbolicating iOS crash reports

Debugging an iOS application crash sometimes involves analyzing crash reports.
More info about crash reports can be found
[in the official documentation](https://developer.apple.com/library/archive/technotes/tn2151/_index.html).

Crash reports generally require symbolication to become properly readable:
symbolication turns machine code addresses into human-readable source locations.
The document below describes some specific details of symbolicating crash reports
from iOS applications using Kotlin.

## Enable .dSYM for release Kotlin binaries

To symbolicate addresses in Kotlin code (e.g. for stack trace elements
corresponding to Kotlin code) `.dSYM` bundle for Kotlin code is required.

By default Kotlin/Native compiler doesn't produce `.dSYM` for release
(i.e. optimized) binaries. This can be changed with `-Xg0` experimental
compiler flag: it enables debug info and `.dSYM` bundle generation for produced
release binaries. To enable it in Gradle, use

```kotlin
kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.all {
            freeCompilerArgs += "-Xg0"
        }
    }
}
```

(in Kotlin DSL).

In projects created from IntelliJ IDEA or AppCode templates these `.dSYM` bundles
are then discovered by Xcode automatically.

## Make frameworks static when using rebuild from bitcode

Rebuilding Kotlin-produced framework from bitcode invalidates the original `.dSYM`.
If it is performed locally, make sure the updated `.dSYM` is used when symbolicating
crash reports.

If rebuilding is performed on App Store side, then `.dSYM` of rebuilt *dynamic* framework
seems discarded and not downloadable from App Store Connect.
So in this case it may be required to make the framework static, e.g. with

```kotlin
kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework> {
            isStatic = true
        }
    }
}
```

(in Kotlin DSL).

## Decode inlined stack frames

Xcode doesn't seem to properly decode stack trace elements of inlined function
calls (these aren't only Kotlin `inline` functions but also functions that are
inlined when optimizing machine code). So some stack trace elements may be
missing. If this is the case, consider using `lldb` to process crash report
that is already symbolicated by Xcode, for example:

```bash
$ lldb -b -o "script import lldb.macosx" -o "crashlog file.crash"
```

This command should output crash report that is additionally processed and
includes inlined stack trace elements.

More details can be found in [LLDB documentation](https://lldb.llvm.org/use/symbolication.html).
