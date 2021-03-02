// FIR_COMPARISON
fun usage(obj: JavaClassWithGetterAndSetter) {
    obj.<caret>
}

// EXIST: {"lookupString":"number","tailText":" (from getNumber()/setNumber())", "allLookupStrings":"getNumber, number, setNumber","itemText":"number"}