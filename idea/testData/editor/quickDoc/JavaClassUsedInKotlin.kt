fun testing() {
    <caret>SomeClass<List<String>>()
}

//INFO: <b>public</b> <b>constructor</b> SomeClass&lt;T : (<(raw) Any?>&lt;Any?&gt;..<(raw) Any?>&lt;*&gt;?)&gt;()<br/>Java declaration:<br/>[light_idea_test_case] public class SomeClass&lt;T extends java.util.List&gt; extends Object
