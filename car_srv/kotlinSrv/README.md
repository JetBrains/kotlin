# Car server

## Building 

move to this directory

    $ ./build.sh

This command run gradle build and execute *npm install* for downloading used js modules.

## Run car server

for run server you can use script

    $ ./run.sh

## Deploying car server

*note:* This feature is under development

*note:* this script use expect script languane. You have to install it

for install expect on ubuntu

    $ sudo apt-get install expect

for deploy server on car use

    $ ./deploy.sh -h {ip_addr} -u {user_name} -p {password}

where:
{ip_addr} - ip address of raspberry.
{user_name} and {password} - user name and password for login on raspberry
