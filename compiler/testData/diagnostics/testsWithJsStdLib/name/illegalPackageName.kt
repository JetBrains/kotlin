// !LANGUAGE: -JsAllowInvalidCharsIdentifiersEscaping
// FIR_IDENTICAL
// FILE: slashes.kt
package a.<!INVALID_CHARACTERS!>`//`<!>.b.<!INVALID_CHARACTERS!>`/`<!>.c
class Slashes

// FILE: space.kt
package <!INVALID_CHARACTERS!>` `<!>
class Space

// FILE: less.kt
package <!INVALID_CHARACTERS!>`<`<!>
class Less

// FILE: more.kt
package <!INVALID_CHARACTERS!>`>`<!>
class More

// FILE: dash.kt
package <!INVALID_CHARACTERS!>`-`<!>
class Dash

// FILE: question.kt
package <!INVALID_CHARACTERS!>`?`<!>
class Question
