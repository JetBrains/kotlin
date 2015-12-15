interface Parent
interface Child : Parent

sealed class Page : Parent {
  object One : Page(), Child
  object Two : Page(), Child
}

// Ok: page is a Parent so it can be easily a Child
fun test(page: Page): Boolean = page is Child
