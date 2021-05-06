import incorrect.directory.My

open class My : <!OTHER_ERROR!>My<!>()

open class Your : His()

open class His : <!OTHER_ERROR!>Your<!>()
