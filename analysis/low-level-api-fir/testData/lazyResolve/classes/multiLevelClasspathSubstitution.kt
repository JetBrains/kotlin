// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: topmost
// FILE: topmost.kt
package org.example

interface Base {
    fun topmost()
}

abstract class Topmost : Base

// MODULE: top(topmost)
// FILE: top.kt
package org.example

interface Base {
    fun top()
}

// FILE: Top.java
package org.example;

public abstract class Top extends Topmost {

}

// MODULE: middle(top)
// FILE: middle.kt
package org.example

interface Base {
    fun middle()
}

abstract class Middle : Top()

// MODULE: bottom(middle)
// FILE: bottom.kt
package org.example

interface Base {
    fun bottom()
}

class Bot<caret>tom : Middle()
