// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR
// CHECK_CALLS_WITH_ANNOTATION: org.jetbrains.kotlin.plugin.sandbox.MyInlineable

// MODULE: main
// FILE: main.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import p3.BookmarkButton

@MyInlineable
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

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
) {
}
