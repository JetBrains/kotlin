import incorrect.directory.My

class My : <!OTHER_ERROR!>My<!>()

class Your : His()

class His : <!OTHER_ERROR!>Your<!>()