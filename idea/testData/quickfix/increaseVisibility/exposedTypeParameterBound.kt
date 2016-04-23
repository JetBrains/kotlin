// "Make 'InternalString' public" "true"
// ACTION: Make 'User' internal
// ACTION: Make 'User' private

internal open class InternalString

class User<T : <caret>User<T, InternalString>, R>