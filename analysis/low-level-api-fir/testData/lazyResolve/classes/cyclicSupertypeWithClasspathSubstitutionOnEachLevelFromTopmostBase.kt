// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: topmost
// FILE: topmost.kt
package org.example

interface Ba<caret>se : TopmostAdditional {
    fun topmost()
}

interface TopmostAdditional : Topmost

interface Topmost : Base

// MODULE: top(topmost)
// FILE: top.kt
package org.example

interface Base : TopAdditional {
    fun top()
}

// FILE: TopAdditional.java
package org.example;

public interface TopAdditional extends Top {

}

// FILE: Top.java
package org.example;

public interface Top extends Topmost {

}

// MODULE: middle(top)
// FILE: middle.kt
package org.example

interface Base : MiddleAdditional {
    fun middle()
}

interface MiddleAdditional : Middle

interface Middle : Top

// MODULE: bottom(middle)
// FILE: bottom.kt
package org.example

interface Base : BottomAdditional {
    fun bottom()
}

interface BottomAdditional : Bottom

interface Bottom : Middle
