package test;

public class DefaultModifier implements WithDefault {

}

interface WithDefault {

    default String getString() { return "str"; }

}
