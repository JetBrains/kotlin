// "Replace with 'NewClass'" "true"

package ppp

@deprecated("", ReplaceWith("NewClass"))
class OldClass

class NewClass

fun foo(): ppp.OldClass<caret>? {
    return null
}
