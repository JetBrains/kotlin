// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: top
// FILE: top.kt
package org.example

interface Base {
    fun top()
}

abstract class Top : Base {
    override fun top() {

    }
}

// MODULE: middle(top)
// FILE: middle.kt
package org.example

interface Base {
    fun middle()
}

abstract class Middle : Top() {
    override fun middle() {

    }
}

// MODULE: bottom(middle)
// FILE: bottom.kt
package org.example

interface Base {
    fun bottom()
}

class Bottom : Middle() {
    override<caret> fun bottom() {

    }
}
