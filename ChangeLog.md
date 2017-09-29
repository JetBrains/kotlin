# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2-Beta

### Android

#### New Features

- [`KT-20051`](https://youtrack.jetbrains.com/issue/KT-20051) Quickfixes to support @Parcelize
#### Fixes

- [`KT-19747`](https://youtrack.jetbrains.com/issue/KT-19747) Android extensions + Parcelable: VerifyError in case of RawValue annotation on a type when it's unknown how to parcel it
- [`KT-19899`](https://youtrack.jetbrains.com/issue/KT-19899) Parcelize: Building with ProGuard enabled
- [`KT-19988`](https://youtrack.jetbrains.com/issue/KT-19988) [Android Extensions] inner class LayoutContainer causes NoSuchMethodError
- [`KT-20002`](https://youtrack.jetbrains.com/issue/KT-20002) Parcelize explodes on LongArray
- [`KT-20019`](https://youtrack.jetbrains.com/issue/KT-20019) Parcelize does not propogate flags argument when writing nested Parcelable
- [`KT-20020`](https://youtrack.jetbrains.com/issue/KT-20020) Parcelize does not use primitive array read/write methods on Parcel
- [`KT-20021`](https://youtrack.jetbrains.com/issue/KT-20021) Parcelize does not serialize Parcelable enum as Parcelable
- [`KT-20022`](https://youtrack.jetbrains.com/issue/KT-20022) Parcelize should dispatch directly to java.lang.Enum when writing an enum.
- [`KT-20034`](https://youtrack.jetbrains.com/issue/KT-20034) Application installation failed (INSTALL_FAILED_DEXOPT) in Android 4.3 devices if I use Parcelize
- [`KT-20057`](https://youtrack.jetbrains.com/issue/KT-20057) Parcelize should use specialized write/create methods where available.
- [`KT-20062`](https://youtrack.jetbrains.com/issue/KT-20062) Parceler should allow otherwise un-parcelable property types in enclosing class.
- [`KT-20170`](https://youtrack.jetbrains.com/issue/KT-20170) UAST: Getting the location of a UIdentifier is tricky

### Compiler

- [`KT-4565`](https://youtrack.jetbrains.com/issue/KT-4565) Support smart casting of safe cast's subject (and also safe call's receiver)
- [`KT-8492`](https://youtrack.jetbrains.com/issue/KT-8492) Null check should work after save call with elvis in condition
- [`KT-9327`](https://youtrack.jetbrains.com/issue/KT-9327) Need a way to check whether a lateinit property was assigned
- [`KT-14138`](https://youtrack.jetbrains.com/issue/KT-14138) Allow lateinit local variables
- [`KT-15461`](https://youtrack.jetbrains.com/issue/KT-15461) Allow lateinit top level properties
- [`KT-7257`](https://youtrack.jetbrains.com/issue/KT-7257) NPE when accessing properties of enum from inner lambda on initialization
- [`KT-9580`](https://youtrack.jetbrains.com/issue/KT-9580) Report an error if 'setparam' target does not make sense for a parameter declaration
- [`KT-16310`](https://youtrack.jetbrains.com/issue/KT-16310) Nested classes inside enum entries capturing outer members
- [`KT-20155`](https://youtrack.jetbrains.com/issue/KT-20155) Confusing diagnostics on a nested interface in inner class

### IDE

- [`KT-14175`](https://youtrack.jetbrains.com/issue/KT-14175) Surround with try ... catch (... finally) doesn't work for expressions
- [`KT-20308`](https://youtrack.jetbrains.com/issue/KT-20308) New Gradle with Kotlin DSL project wizard
- [`KT-18353`](https://youtrack.jetbrains.com/issue/KT-18353) Support UAST for .kts files
- [`KT-19823`](https://youtrack.jetbrains.com/issue/KT-19823) Kotlin Gradle project import into IntelliJ: import kapt generated classes into classpath
- [`KT-20185`](https://youtrack.jetbrains.com/issue/KT-20185) Stub and PSI element type mismatch for "var nullableSuspend: (suspend (P) -> Unit)? = null"

### Language design

- [`KT-14486`](https://youtrack.jetbrains.com/issue/KT-14486) Allow smart cast in closure if a local variable is modified only before it (and not after or inside)
- [`KT-15667`](https://youtrack.jetbrains.com/issue/KT-15667) Support "::foo" as a short-hand syntax for bound callable reference to "this::foo"
- [`KT-16681`](https://youtrack.jetbrains.com/issue/KT-16681) kotlin allows mutating the field of read-only property

### Libraries

- [`KT-19258`](https://youtrack.jetbrains.com/issue/KT-19258) Java 9: module-info.java with `requires kotlin.stdlib` causes compiler to fail: "module reads package from both kotlin.reflect and kotlin.stdlib"

### Tools

- [`KT-19692`](https://youtrack.jetbrains.com/issue/KT-19692) kotlin-jpa plugin doesn't support @MappedSuperclass annotation
- [`KT-20030`](https://youtrack.jetbrains.com/issue/KT-20030) Parcelize can directly reference writeToParcel and CREATOR for final, non-Parcelize Parcelable types in same compilation unit.
- [`KT-19742`](https://youtrack.jetbrains.com/issue/KT-19742) [Android extensions] Calling clearFindViewByIdCache causes NPE
- [`KT-19749`](https://youtrack.jetbrains.com/issue/KT-19749) Android extensions + Parcelable: NoSuchMethodError on attempt to pack into parcel a serializable object
- [`KT-20026`](https://youtrack.jetbrains.com/issue/KT-20026) Parcelize overrides describeContents despite being already implemented.
- [`KT-20027`](https://youtrack.jetbrains.com/issue/KT-20027) Parcelize uses wrong classloader when reading parcelable type.
- [`KT-20029`](https://youtrack.jetbrains.com/issue/KT-20029) Parcelize should not directly reference parcel methods on types outside compilation unit
- [`KT-20032`](https://youtrack.jetbrains.com/issue/KT-20032) Parcelize does not respect type nullability in case of Parcelize parcelables

### Tools. CLI

- [`KT-10563`](https://youtrack.jetbrains.com/issue/KT-10563) Support a command line argument -Werror to treat warnings as errors

### Tools. Gradle

- [`KT-20212`](https://youtrack.jetbrains.com/issue/KT-20212) Cannot access internal components from test code

### Tools. kapt

- [`KT-17923`](https://youtrack.jetbrains.com/issue/KT-17923) Reference to Dagger generated class is highlighted red
- [`KT-18923`](https://youtrack.jetbrains.com/issue/KT-18923) Kapt: Do not use the Kotlin error message collector to issue errors from kapt
- [`KT-19097`](https://youtrack.jetbrains.com/issue/KT-19097) Request: Decent support of `kapt.kotlin.generated` on Intellij/Android Studio
- [`KT-20001`](https://youtrack.jetbrains.com/issue/KT-20001) kapt generate stubs Gradle task does not depend on the compilation of sub-project kapt dependencies

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.1.50`](https://github.com/JetBrains/kotlin/blob/1.1.50/ChangeLog.md) release.
