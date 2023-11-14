<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.WARNING) fun TestWarning()<!> {}
<!CONFLICTING_OVERLOADS!>fun TestWarning()<!> {}

<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.ERROR) fun TestError()<!> {}
<!CONFLICTING_OVERLOADS!>fun TestError()<!> {}

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestHidden() {}
fun TestHidden() {}
