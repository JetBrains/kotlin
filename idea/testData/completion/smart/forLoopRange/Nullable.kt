fun foo(p: String?) {
    for (i in <caret>)
}

fun f(): Collection<Int>? {}

// ABSENT: { lookupString:"p", itemText: "p" }
// EXIST: { lookupString:"p", itemText: "!! p", typeText:"String?" }
// EXIST: { lookupString:"p", itemText: "?: p", typeText:"String?" }
// ABSENT: { lookupString:"f", itemText: "f" }
// EXIST: { lookupString:"f", itemText: "!! f", typeText:"Collection<Int>?" }
// EXIST: { lookupString:"f", itemText: "?: f", typeText:"Collection<Int>?" }
