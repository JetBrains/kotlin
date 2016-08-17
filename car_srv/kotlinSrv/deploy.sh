#!/usr/bin/expect

# XXX this script works super slow on eugene's machine

set host ""
set userName "pi"
set password "111"

set helpMessage "Options:\n
-h (host) - set ip address of car computer\n
-u (user name) - set user for login on car computer. default 'pi'\n
-p (password) - set password for user. default '111'\n

e.g. ./deploy.sh -h 192.168.1.117 -u testuser -p 123456789\n\n"

set paramName ""

foreach arg $argv {
    switch -- $arg {
        "--help" {
            send "$helpMessage" 
            exit 1
        }
        "-h" {
            set paramName "host"
        }
        "-p" {
            set paramName "password"
        }
        "-u" {
            set paramName "userName"
        }
        default {
            if {$paramName==""} {
                send "Error. Incorrect option $arg\n" 
                send "$helpMessage" 
                exit 1
            }
            set $paramName $arg
            set paramName ""
        }
    }
}
if {$host==""} {
    send "Error. Host param required\n" 
    send "$helpMessage" 
    exit 1
}

set timeout 3

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
send "killall -9 node \r"
expect " $"
send "cd server \r"
expect " $"
set timeout 120
send "npm install \r"
expect " $"
#send "node main.js &\r"
#expect " $"

send "exit\r"
expect eof
