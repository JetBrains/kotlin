package ppp

class Base(p: Base.N<caret>) {
    class Nested
}

// EXIST: { lookupString: "Nested", itemText: "Nested", tailText: " (ppp.Base)" }
