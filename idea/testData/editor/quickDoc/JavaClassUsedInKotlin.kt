fun testing() {
    <caret>SomeClass<List<String>>()
}

// INFO: <b>public</b> <b>constructor</b> SomeClass&lt;T : (MutableList&lt;Any?&gt;..List&lt;*&gt;?)&gt;()<br/>Java declaration:<br/>[light_idea_test_case] public class SomeClass&lt;T extends java.util.List&gt; extends Object