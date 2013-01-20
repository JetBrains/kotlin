// "Add 'open' modifier to supertype" "false"
// ERROR: This type is final, so it cannot be inherited from
class A : String<caret>() {}
