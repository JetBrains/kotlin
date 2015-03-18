fun ktTest() {
    Test.<caret>foo("SomeTest")
}

//INFO: <b>public</b> <b>open</b> <b>fun</b> foo(param: String!): Array&lt;(out) Any!&gt;!<br/>Java declaration:<br/>Test...
