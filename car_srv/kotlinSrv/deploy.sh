#!/usr/bin/expect


#todo make as script params
set host "192.168.43.135"
set userName "pi"
set password "111"

spawn ./gradlew build
expect eof

spawn scp -r ./build/js/main.js ./build/js/kotlin.js ./build/js/package.json ./build/js/proto/ $userName@$host:./server/

expect "password:"
send "$password\r"
expect eof

spawn ssh $userName@$host
expect "password:"
send "$password\r"

expect "%"
send "killall -9 node"

expect "%"
send "cd server \r"
expect "%"
send "npm install \r"
expect "%"
send "node main.js &\r"
expect "%"
send "exit\r"
expect eof
