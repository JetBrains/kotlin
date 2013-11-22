fun testing() {
    <caret>SomeClass<List<String>>()
}

// INFO: <b>public</b> <b>constructor</b> SomeClass&lt;T : jet.List&lt;jet.Any?&gt;?>() <i>defined in</i> SomeClass<br/>Java declaration:<br/>[light_idea_test_case] public class SomeClass<T extends java.util.List> extends Object