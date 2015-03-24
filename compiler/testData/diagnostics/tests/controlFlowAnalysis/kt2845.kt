//KT-2845 Wrong cf-analysys for variable initialization in try..finally
package h

import java.util.ArrayList

private fun doTest() : Int {
    var list : MutableList<Int>? ;
    try {
        list = ArrayList()
        // Not-null was just assigned to the list
        <!DEBUG_INFO_SMARTCAST!>list<!>.add(3)
        return 0 ;
    }
    finally {
        if(<!UNINITIALIZED_VARIABLE!>list<!> != null) { // Must be an ERROR
        }
    }
}
