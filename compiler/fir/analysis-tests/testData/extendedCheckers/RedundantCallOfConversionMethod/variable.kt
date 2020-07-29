// WITH_RUNTIME
val foo = ""
val bar = foo.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>