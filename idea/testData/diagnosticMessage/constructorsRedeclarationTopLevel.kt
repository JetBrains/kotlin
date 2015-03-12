// !DIAGNOSTICS_NUMBER: 2
// !DIAGNOSTICS: CONFLICTING_OVERLOADS
// !MESSAGE_TYPE: TEXT
fun Element(x: String) {}
class Element {
    constructor(x : String) {}
}