// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR
// CHECK_CALLS_WITH_ANNOTATION: org.jetbrains.kotlin.fir.plugin.MyComposable

// MODULE: main
// FILE: main.kt
import org.jetbrains.kotlin.fir.plugin.MyComposable
import p3.BookmarkButton

@MyComposable
fun PostCardSimple(
    navigateToArticle: (String) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    BookmarkButton(
        isBookmarked = isFavorite,
        onClick = onToggleFavorite,
    )
}
// FILE: utils/JetnewsIcons.kt
package p3

import org.jetbrains.kotlin.fir.plugin.MyComposable

@MyComposable
fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
) {
}