# Car server

## Deploying car server

*note:* this script use expect script language. You have to install it

for install expect on ubuntu

    $ sudo apt-get install expect

for deploy server on car use

    $ ./deploy.sh -h {ip_addr} -u {user_name} -p {password}

where:
{ip_addr} - ip address of raspberry.
{user_name} and {password} - user name and password for login on raspberry


## Config file

on root of compile files (~/server/ after deploy or ./build/js/) you can create
config file config.cfg. options write as key:value.
all available options. here value - its default value
mainServerIp:127.0.0.1
thisServerIp:127.0.0.1


## Building and run (for start on local computer)

move to this directory

    $ ./build.sh

This command run gradle build and execute *npm install* for downloading used js modules.

for run server you can use script

    $ ./run.sh


