#!/bin/bash
# TODO main server ip address should be read from configuration file that is
# not overwritten during deploy.
cd build/js
node main.js
