@Deprecated(message = "", level = DeprecationLevel.WARNING) <!CONFLICTING_OVERLOADS!>fun TestWarning()<!> {}
<!CONFLICTING_OVERLOADS!>fun TestWarning()<!> {}

@Deprecated(message = "", level = DeprecationLevel.ERROR) <!CONFLICTING_OVERLOADS!>fun TestError()<!> {}
<!CONFLICTING_OVERLOADS!>fun TestError()<!> {}

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestHidden() {}
fun TestHidden() {}
