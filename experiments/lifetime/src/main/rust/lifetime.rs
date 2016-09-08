/**
 * In terms of C++, rust lack of constuctor ... as alternative there is Default traits to
 * initilize object with some default values
 * bash-3.2$ rustc -A unused_variables lifetime.rs
 *
 * bash-3.2$ ./lifetime
main started
scope started
B(0x7fff571df328)::drop
scope ended
main ended
B(0x7fff571df3a0)::drop
B(0x7fff571df3a8)::drop
 */

struct A;

struct B;


impl Drop for B {
  fn drop(&mut self) {
    println! ("B({:p})::drop", self);
  }
}

fn main() {
   println!("main started");
   let x = A;
   let a = B;
   let b = B;
   println!("x:{:p}, a:{:p}, b:{:p}", &x, &a, &b);
   println!("scope started");
   {
	let c = B;
	println!("c:{:p}", &c);
   }
   println!("scope ended");
   println!("main ended");
}
