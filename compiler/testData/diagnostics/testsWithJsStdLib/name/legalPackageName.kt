// FIR_IDENTICAL
// !LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// FILE: slashes.kt
package a.`//`.b.`/`.c
class Slashes

// FILE: slash.kt
package `/`
class Slash

// FILE: space.kt
package ` `
class Space

// FILE: less.kt
package `<`
class Less

// FILE: more.kt
package `>`
class More

// FILE: dash.kt
package `-`
class Dash

// FILE: question.kt
package `?`
class Question
