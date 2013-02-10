// "Remove 'internal' modifier" "false"
// ACTION: Remove 'private' modifier
// ACTION: Remove 'public' modifier
// ERROR: Incompatible modifiers: 'private public'
// ERROR: Incompatible modifiers: 'private public'
class A {
    <caret>private public fun f() {}
}