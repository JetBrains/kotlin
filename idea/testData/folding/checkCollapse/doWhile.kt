fun foo() <fold text='{...}' expand='true'>{
    do <fold text='{...}
' expand='true'>{
    }</fold> while (true)
    do <fold text='{...}' expand='true'>{
    }</fold>
    while (true)
}</fold>

// SET_TRUE: setCollapseImports