fun foo() <fold text='{...}' expand='true'>{
    try <fold text='{...}
' expand='true'>{
        assert(false)
    }</fold> catch (e: java.lang.AssertionError) <fold text='{...}
' expand='true'>{
        throw e
    }</fold> finally <fold text='{...}' expand='true'>{
    }</fold>
    try <fold text='{...}' expand='true'>{
        assert(false)
    }</fold>
    catch (e: java.lang.AssertionError) <fold text='{...}' expand='true'>{
        throw e
    }</fold>
    finally <fold text='{...}' expand='true'>{
        throw e
    }</fold>
}</fold>

// SET_TRUE: setCollapseImports