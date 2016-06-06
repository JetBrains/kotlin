fun foo() <fold text='{...}' expand='true'>{
    if (true) <fold text='{...}
' expand='true'>{
    }</fold> else if (true) <fold text='{...}
' expand='true'>{
    }</fold> else if (false) <fold text='{...}' expand='true'>{
    }</fold>

    if (true) <fold text='{...}' expand='true'>{
    }</fold>
    else if (true) <fold text='{...}' expand='true'>{
    }</fold>
    else if (false) <fold text='{...}' expand='true'>{
    }</fold>

    if (true) <fold text='{...}' expand='true'>{
        assert(true)
    }</fold>
}</fold>

// SET_TRUE: setCollapseImports