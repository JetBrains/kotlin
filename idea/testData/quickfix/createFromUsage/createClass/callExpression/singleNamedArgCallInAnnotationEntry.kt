// "Create annotation 'bar'" "true"
// ERROR: Unresolved reference: foo

@[foo(1, "2", <caret>bar(fooBar = "3"))] fun test() {

}