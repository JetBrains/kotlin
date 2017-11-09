package test;

public class Inheritance extends AbstractInheritance { }

abstract class AbstractInheritance implements Interface {

    @Override public int getAnswer() { return 42; }

}

interface Interface extends I {
    @Override
    int getAnswer();
}

interface I {
    int getAnswer();
}

interface I2 extends I {

    @Override
    int getAnswer();

}
