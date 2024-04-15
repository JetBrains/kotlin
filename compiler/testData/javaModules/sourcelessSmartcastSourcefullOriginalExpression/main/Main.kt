// Inspired by:
// intellij-2022-new/testProject/community/android/android-test-framework/testSrc/com/android/tools/idea/testing/ProjectViewUtils.kt
// Project.dumpAndroidProjectView() -> PresentationData.toTestText() -> Icon.toText()
//     -> this is ImageIconUIResource -> description ?: "ImageIconUIResource(?)"

fun Any.test() = when {
    this is sun.swing.ImageIconUIResource -> description
    else -> "emptyness"
}
