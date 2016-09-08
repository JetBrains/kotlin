/**
 * Go doesn't have object life event's, but it has defered execution et the "end of scope"  
bash-3.2$ /usr/local/go/bin/go build lifetime.go
bash-3.2$ ./lifetime
main: started
defer-foo1: started
defer-foo1: ended
defer-foo0: started
defer-foo0: ended
inner-main: started
inner-main: ended
main: ended
defer-inner-main: started
defer-inner-main: ended
defer-main: started
defer-main: ended
 */
package main

import "fmt"

func foo() {
     defer func() {
      	   fmt.Println("defer-foo0: started");
     	   fmt.Println("defer-foo0: ended");
     }()
     defer func() {
     	   fmt.Println("defer-foo1: started");
     	   fmt.Println("defer-foo1: ended");
     }()

}

func main() {
  fmt.Println("main: started");
  foo()
  defer func() {
    	fmt.Println("defer-main: started");
  	fmt.Println("defer-main: ended");
  }()
  {
    fmt.Println("inner-main: started");
    defer func(){
      fmt.Println("defer-inner-main: started");
      fmt.Println("defer-inner-main: ended");
    }()
  }
  fmt.Println("inner-main: ended");
  fmt.Println("main: ended");
}
