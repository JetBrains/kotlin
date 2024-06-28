// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: top
// FILE: top.kt
package org.example

interface Base {
    fun top()
}

abstract class Top : Base

// MODULE: middle(top)
// FILE: middle.kt
package org.example

interface Base {
    fun middle()
}

abstract class Middle : Top() {
    override fun mid<caret>dle() {

    }
}

// MODULE: bottom(middle)
// FILE: bottom.kt
package org.example

interface Base {
    fun bottom()
}

class Bottom : Middle() {
    overri<caret_preresolved>de fun bottom() {

    }
}
