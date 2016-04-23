// "Make 'User' internal" "true"
// ACTION: Make 'InternalString' public

internal open class InternalString

class User<T : <caret>User<T, InternalString>, R>