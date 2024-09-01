// JDK_KIND: FULL_JDK_11

// Inspired by:
// intellij-2022-new/testProject/community/android/android-test-framework/testSrc/com/android/tools/idea/testing/ProjectViewUtils.kt
// Project.dumpAndroidProjectView() -> PresentationData.toTestText() -> Icon.toText()
//     -> this is ImageIconUIResource -> description ?: "ImageIconUIResource(?)"

fun Any.test() = when {
    this is <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>sun.swing.ImageIconUIResource<!> -> description
    else -> "emptyness"
}
