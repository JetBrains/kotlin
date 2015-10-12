annotation class Ann

class Completion(@get:Ann val p1: String, @<caret>)

// EXIST: get
// EXIST: set
// EXIST: field
// EXIST: param
// EXIST: setparam
// EXIST: property

/*TODO: keywords below should not be here*/
// EXIST: val
// EXIST: var
// EXIST: receiver
// NOTHING_ELSE
