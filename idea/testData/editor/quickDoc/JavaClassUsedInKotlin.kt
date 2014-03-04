fun testing() {
    <caret>SomeClass<List<String>>()
}

// INFO: <b>public</b> <b>constructor</b> SomeClass&lt;T : kotlin.List&lt;kotlin.Any?&gt;?>() <i>defined in</i> SomeClass<br/>Java declaration:<br/>[light_idea_test_case] public class SomeClass&lt;T extends java.util.List&gt; extends Object