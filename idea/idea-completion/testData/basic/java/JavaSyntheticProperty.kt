// FIR_COMPARISON
fun foo(a: java.lang.Thread) {
    a.na<caret>
}


// EXIST: {"lookupString":"name","tailText":" (from getName()/setName())","allLookupStrings":"getName, name, setName","itemText":"name"}
