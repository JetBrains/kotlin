package _06_classy._47_Well_Dog_My_Cats;

class Counter {
    private static int count = 0;
    public static final synchronized void increment() {
        count++;
    }
    public static final synchronized int getCount() {
        return count; 
    } 
}

class Dog extends Counter {
    public Dog() { }
    public void woof() { increment(); }
} 

class Cat extends Counter {
    public Cat() { } 
    public void meow() { increment(); }
}

public class Ruckus {
    public static void main(String[] args) { 
        Dog dogs[] = { new Dog(), new Dog() };
        for (int i = 0; i < dogs.length; i++)
            dogs[i].woof();
        Cat cats[] = { new Cat(), new Cat(), new Cat() };
        for (int i = 0; i < cats.length; i++)
            cats[i].meow();
        System.out.print(Dog.getCount() + " woofs and ");
        System.out.println(Cat.getCount() + " meows");
    }
}
